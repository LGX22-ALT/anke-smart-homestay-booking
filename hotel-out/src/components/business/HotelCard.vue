<script setup>
defineProps({
  hotel: {
    type: Object,
    required: true,
  },
})
</script>

<template>
  <article class="hotel-card">
    <RouterLink
      class="hotel-image-link"
      :to="{ name: 'hotel-detail', params: { id: hotel.id } }"
      :aria-label="`查看${hotel.name}`"
    >
      <div class="hotel-image" :style="{ background: hotel.fallback }">
        <img
          :src="hotel.image"
          :alt="hotel.name"
          loading="eager"
          @error="(event) => (event.target.style.display = 'none')"
        />
        <span class="image-label">精选房源</span>
      </div>
    </RouterLink>
    <div class="hotel-content">
      <div class="hotel-heading">
        <div>
          <p class="hotel-location">{{ hotel.location }}</p>
          <h3>{{ hotel.name }}</h3>
        </div>
        <span class="rating">{{ hotel.rating }}</span>
      </div>
      <p class="hotel-description">{{ hotel.description }}</p>
      <div class="hotel-meta">
        <div class="hotel-tags">
          <span v-for="tag in hotel.tags" :key="tag">
            {{ tag }}
          </span>
        </div>
        <p class="hotel-price">
          <strong>¥{{ hotel.price }}</strong>
          <span>/晚起</span>
        </p>
      </div>
    </div>
  </article>
</template>

<style scoped>
.hotel-card {
  overflow: hidden;
  border: 1px solid var(--line);
  background: #fff;
  box-shadow: 0 16px 40px rgb(16 42 67 / 7%);
  transition:
    transform 220ms ease,
    box-shadow 220ms ease;
}

.hotel-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 20px 48px rgb(16 42 67 / 13%);
}

.hotel-image-link {
  display: block;
  text-decoration: none;
}

.hotel-image {
  position: relative;
  aspect-ratio: 1.35;
  overflow: hidden;
}

.hotel-image::after {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 55%, rgb(16 42 67 / 30%));
  content: '';
}

.hotel-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 400ms ease;
}

.hotel-card:hover .hotel-image img {
  transform: scale(1.04);
}

.image-label {
  position: absolute;
  z-index: 1;
  right: 14px;
  bottom: 13px;
  color: #fff;
  font-size: 11px;
  letter-spacing: 0.08em;
}

.hotel-content {
  padding: 20px;
}

.hotel-heading {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 12px;
}

.hotel-location {
  margin: 0 0 7px;
  color: var(--muted);
  font-size: 12px;
  letter-spacing: 0.04em;
}

h3 {
  margin: 0;
  color: var(--ink);
  font-family: var(--font-ui);
  font-size: 22px;
  font-weight: 700;
}

.rating {
  padding: 5px 7px;
  color: #9b5c16;
  background: #fff4d6;
  font-size: 13px;
  font-weight: 700;
}

.hotel-description {
  display: -webkit-box;
  overflow: hidden;
  margin: 14px 0 18px;
  color: #52606d;
  font-size: 13px;
  line-height: 1.7;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.hotel-meta {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 12px;
}

.hotel-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.hotel-tags span {
  padding: 4px 7px;
  color: var(--brand);
  background: #e9f5f3;
  font-size: 11px;
}

.hotel-price {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin: 0;
  white-space: nowrap;
}

.hotel-price strong {
  color: var(--ink);
  font-size: 21px;
}

.hotel-price span {
  color: var(--muted);
  font-size: 11px;
}
</style>
