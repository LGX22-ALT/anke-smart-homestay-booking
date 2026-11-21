package com.school.hotel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.hotel.common.BusinessException;
import com.school.hotel.dto.AiSettingsRequest;
import com.school.hotel.dto.AiConnectionRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.*;

/** Server-only configuration. Never return the stored credential to a browser. */
@Service
public class AiSettingsService {
    public record Settings(boolean enabled, String endpoint, String model, String apiKey, int maxTokens, double temperature, String mode) {
        public Settings(boolean enabled, String endpoint, String model, String apiKey) {
            this(enabled, endpoint, model, apiKey, 4096, 0.6, "quick");
        }
        public Settings withMode(String mode) { return new Settings(enabled, endpoint, model, apiKey, maxTokens, temperature, mode); }
    }
    public record Connection(String id, String name, String provider, String baseUrl, Settings settings) {}
    public record Library(int version, String activeId, List<Connection> connections) {}
    private Library library;
    private final ObjectMapper mapper;
    private final Path file;
    private Settings settings;

    public AiSettingsService(ObjectMapper mapper,
            @Value("${hotel.ai.config-path:./data/ai-settings.json}") String path) {
        this.mapper = mapper;
        this.file = Path.of(path).toAbsolutePath().normalize();
        try {
            if (Files.exists(file)) {
                var node = mapper.readTree(file.toFile());
                if (node.has("connections")) {
                    library = mapper.treeToValue(node, Library.class);
                    settings = library.connections().stream().filter(c -> c.id().equals(library.activeId()))
                            .map(Connection::settings).findFirst().orElse(new Settings(false,"","",""));
                } else settings = new Settings(node.path("enabled").asBoolean(),node.path("endpoint").asText(""),node.path("model").asText(""),node.path("apiKey").asText(""));
            } else settings = new Settings(false, "", "", "");
            if (library == null) {
                boolean exists = !settings.endpoint().isBlank();
                library = new Library(1, exists ? "legacy" : "", exists ? List.of(new Connection("legacy",settings.model(),"custom",base(settings.endpoint()),settings)) : List.of());
            }
        } catch (IOException ex) {
            // Fail closed rather than silently overwriting an unreadable credential file.
            throw new IllegalStateException("无法读取 AI 配置文件，请检查后端文件权限或配置格式");
        }
    }

    public synchronized Settings current() { return settings; }

    private static String base(String value) { return value.replaceFirst("/chat/completions/?$", "").replaceFirst("/+$", ""); }
    private Map<String,Object> connectionView(Connection c) {
        var result = new LinkedHashMap<String,Object>();
        result.put("id",c.id()); result.put("name",c.name()); result.put("provider",c.provider());
        result.put("baseUrl",c.baseUrl()); result.put("model",c.settings().model());
        result.put("enabled",c.settings().enabled()); result.put("hasApiKey",!c.settings().apiKey().isBlank());
        result.put("active",c.id().equals(library.activeId()));
        result.put("maxTokens",c.settings().maxTokens()); result.put("temperature",c.settings().temperature());
        return result;
    }
    public synchronized List<Map<String,Object>> connections() { return library.connections().stream().map(this::connectionView).toList(); }
    private Connection connection(String id) { return library.connections().stream().filter(c -> c.id().equals(id)).findFirst().orElseThrow(() -> new BusinessException(404,"模型连接不存在")); }
    public synchronized Settings connectionCandidate(String id, AiConnectionRequest request) {
        String url = base(request.getBaseUrl().trim());
        String endpoint = url + "/chat/completions";
        validateEndpoint(endpoint);
        Connection old = id == null ? null : connection(id);
        String key = request.getApiKey() == null ? "" : request.getApiKey().trim();
        if (key.isEmpty() && old != null && old.baseUrl().equals(url)) key = old.settings().apiKey();
        if (key.isBlank() || key.contains("\n") || key.contains("\r")) throw new BusinessException(400,"请填写当前接口的 API 密钥");
        return new Settings(request.isEnabled(),endpoint,request.getModel().trim(),key,request.getMaxTokens(),request.getTemperature(),"quick");
    }
    public synchronized Map<String,Object> saveConnection(String id, AiConnectionRequest request) {
        Settings config = connectionCandidate(id,request);
        String nextId = id == null ? UUID.randomUUID().toString() : id;
        var next = new Connection(nextId,request.getName().trim(),request.getProvider(),base(config.endpoint()),config);
        var entries = new ArrayList<>(library.connections()); entries.removeIf(c -> c.id().equals(nextId)); entries.add(next);
        String active = library.activeId();
        if (active.isBlank() && config.enabled()) active = nextId;
        if (active.equals(nextId) && !config.enabled()) throw new BusinessException(400,"请先切换其他模型再停用当前连接");
        persistLibrary(new Library(1,active,entries)); return connectionView(next);
    }
    public synchronized Map<String,Object> activate(String id) {
        var target = connection(id);
        if (!target.settings().enabled() || target.settings().apiKey().isBlank()) throw new BusinessException(400,"请先启用连接并配置密钥");
        persistLibrary(new Library(1,id,library.connections())); return connectionView(target);
    }
    public synchronized void deleteConnection(String id) {
        connection(id);
        if (id.equals(library.activeId())) throw new BusinessException(400,"请先切换其他模型，再删除当前连接");
        persistLibrary(new Library(1,library.activeId(),library.connections().stream().filter(c -> !c.id().equals(id)).toList()));
    }
    public synchronized Settings savedConnection(String id) { return connection(id).settings(); }
    private void persistLibrary(Library next) {
        Path temp = null;
        try {
            Files.createDirectories(file.getParent());
            temp = Files.createTempFile(file.getParent(),"ai-library-",".tmp");
            mapper.writeValue(temp.toFile(),next);
            try { Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING); }
            library = next;
            settings = next.connections().stream().filter(c -> c.id().equals(next.activeId())).map(Connection::settings).findFirst().orElse(new Settings(false,"","",""));
        } catch(IOException ex) { throw new BusinessException(500,"模型连接保存失败，请检查目录权限"); }
        finally { if(temp != null) try { Files.deleteIfExists(temp); } catch(IOException ignored) {} }
    }

    public synchronized Map<String, Object> view() {
        return Map.of("enabled", settings.enabled(), "endpoint", settings.endpoint(),
                "model", settings.model(), "hasApiKey", !settings.apiKey().isBlank());
    }

    public synchronized Settings candidate(AiSettingsRequest request, boolean forTest) {
        String endpoint = request.getEndpoint() == null ? "" : request.getEndpoint().trim();
        String key = request.getApiKey() != null && !request.getApiKey().isBlank()
                ? request.getApiKey().trim() : request.isClearApiKey() || !endpoint.equals(settings.endpoint())
                ? "" : settings.apiKey();
        String model = request.getModel() == null ? "" : request.getModel().trim();
        if (request.isEnabled() || forTest) {
            validateEndpoint(endpoint);
            if (model.isBlank() || key.isBlank() || key.contains("\n") || key.contains("\r"))
                throw new BusinessException(400, "请填写模型名称和有效的 API 密钥");
        }
        return new Settings(request.isEnabled(), endpoint, model, key);
    }

    public synchronized Map<String, Object> save(AiSettingsRequest request) {
        Settings next = candidate(request, false);
        if (library != null) {
            String id = library.activeId().isBlank() ? "legacy" : library.activeId();
            var entries = new ArrayList<>(library.connections());
            entries.removeIf(c -> c.id().equals(id));
            entries.add(new Connection(id,next.model(),"custom",base(next.endpoint()),next));
            persistLibrary(new Library(1,id,entries)); return view();
        }
        Path temp = null;
        try {
            Files.createDirectories(file.getParent());
            temp = Files.createTempFile(file.getParent(), "ai-settings-", ".tmp");
            mapper.writeValue(temp.toFile(), next);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
            settings = next;
            return view();
        } catch (IOException ex) {
            throw new BusinessException(500, "AI 配置保存失败，请检查后端数据目录的写入权限");
        } finally {
            if (temp != null) try { Files.deleteIfExists(temp); } catch (IOException ignored) { }
        }
    }

    private void validateEndpoint(String endpoint) {
        try {
            URI uri = URI.create(endpoint);
            // HTTP is only allowed for a local model/test server; remote credentials require TLS.
            boolean local = "http".equals(uri.getScheme()) &&
                    ("localhost".equals(uri.getHost()) || "127.0.0.1".equals(uri.getHost()));
            if ((!"https".equals(uri.getScheme()) && !local) || uri.getHost() == null
                    || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                    || !uri.getPath().endsWith("/chat/completions")) throw new IllegalArgumentException();
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(400, "请填写完整的 HTTPS Chat Completions 地址，以 /chat/completions 结尾");
        }
    }
}
