<template>
  <section class="trajectory-map" aria-label="人物轨迹地图">
    <div class="section-head">
      <div>
        <h3>人物轨迹</h3>
        <p class="helper-text">按时间顺序连接已填写地点的生命节点。</p>
      </div>
      <span class="section-badge">{{ mappedPoints.length }} 个地点</span>
    </div>

    <div class="trajectory-map__canvas">
      <svg viewBox="0 0 720 460" role="img" aria-label="人物轨迹地图视图">
        <defs>
          <linearGradient id="trajectorySea" x1="0" x2="1" y1="0" y2="1">
            <stop offset="0%" stop-color="#f9efe5" />
            <stop offset="100%" stop-color="#edf5f3" />
          </linearGradient>
        </defs>
        <rect width="720" height="460" rx="18" fill="url(#trajectorySea)" />
        <path
          :d="chinaOutlinePath"
          class="trajectory-map__outline"
        />
        <g class="trajectory-map__grid">
          <line v-for="x in gridX" :key="`x-${x}`" :x1="x" :x2="x" y1="28" y2="432" />
          <line v-for="y in gridY" :key="`y-${y}`" x1="28" x2="692" :y1="y" :y2="y" />
        </g>
        <polyline
          v-if="mappedPoints.length > 1"
          class="trajectory-map__route"
          :points="routePoints"
        />
        <g
          v-for="(point, index) in mappedPoints"
          :key="point.id"
          class="trajectory-map__point"
        >
          <circle :cx="point.x" :cy="point.y" r="8" />
          <text :x="point.x + 12" :y="point.y - 8">{{ index + 1 }}. {{ point.location }}</text>
          <text class="trajectory-map__year" :x="point.x + 12" :y="point.y + 10">{{ point.year }} · {{ point.title }}</text>
        </g>
      </svg>

      <div v-if="!mappedPoints.length" class="trajectory-map__empty">
        <strong>暂无可绘制地点</strong>
        <span>给时间节点补充城市或省份后，这里会自动生成轨迹。</span>
      </div>
    </div>

    <div v-if="unmappedEntries.length" class="trajectory-map__unmapped">
      <span>未匹配地点：</span>
      <strong>{{ unmappedEntries.map((entry) => `${entry.year} ${entry.location || entry.title}`).join(" / ") }}</strong>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from "vue";

import type { TimelineEntry } from "@/types/api";

const props = defineProps<{
  timeline?: TimelineEntry[];
  fallbackLocation?: string;
}>();

const cityCoordinates: Record<string, [number, number]> = {
  Beijing: [116.4074, 39.9042],
  上海: [121.4737, 31.2304],
  Shanghai: [121.4737, 31.2304],
  广州: [113.2644, 23.1291],
  Guangzhou: [113.2644, 23.1291],
  深圳: [114.0579, 22.5431],
  Shenzhen: [114.0579, 22.5431],
  成都: [104.0665, 30.5728],
  Chengdu: [104.0665, 30.5728],
  广元: [105.8436, 32.4355],
  Guangyuan: [105.8436, 32.4355],
  重庆: [106.5516, 29.563],
  Chongqing: [106.5516, 29.563],
  西安: [108.9398, 34.3416],
  Xian: [108.9398, 34.3416],
  南京: [118.7969, 32.0603],
  Nanjing: [118.7969, 32.0603],
  杭州: [120.1551, 30.2741],
  Hangzhou: [120.1551, 30.2741],
  武汉: [114.3055, 30.5928],
  Wuhan: [114.3055, 30.5928],
  长沙: [112.9388, 28.2282],
  Changsha: [112.9388, 28.2282],
  昆明: [102.8329, 24.8801],
  Kunming: [102.8329, 24.8801],
  贵阳: [106.6302, 26.647],
  Guiyang: [106.6302, 26.647],
  拉萨: [91.1175, 29.647],
  Lhasa: [91.1175, 29.647],
  乌鲁木齐: [87.6168, 43.8256],
  Urumqi: [87.6168, 43.8256],
  哈尔滨: [126.5349, 45.8038],
  Harbin: [126.5349, 45.8038],
  沈阳: [123.4315, 41.8057],
  Shenyang: [123.4315, 41.8057],
  天津: [117.2009, 39.0842],
  Tianjin: [117.2009, 39.0842],
  郑州: [113.6254, 34.7466],
  Zhengzhou: [113.6254, 34.7466],
  济南: [117.1201, 36.6512],
  Jinan: [117.1201, 36.6512],
  福州: [119.2965, 26.0745],
  Fuzhou: [119.2965, 26.0745],
  厦门: [118.0894, 24.4798],
  Xiamen: [118.0894, 24.4798],
  南昌: [115.8582, 28.6829],
  Nanchang: [115.8582, 28.6829],
  合肥: [117.2272, 31.8206],
  Hefei: [117.2272, 31.8206],
  兰州: [103.8343, 36.0611],
  Lanzhou: [103.8343, 36.0611],
  西宁: [101.7782, 36.6171],
  Xining: [101.7782, 36.6171],
  银川: [106.2309, 38.4872],
  Yinchuan: [106.2309, 38.4872],
  呼和浩特: [111.7492, 40.8426],
  Hohhot: [111.7492, 40.8426],
  南宁: [108.3669, 22.817],
  Nanning: [108.3669, 22.817],
  海口: [110.1983, 20.044],
  Haikou: [110.1983, 20.044],
  台北: [121.5654, 25.033],
  Taipei: [121.5654, 25.033],
  香港: [114.1694, 22.3193],
  HongKong: [114.1694, 22.3193],
  澳门: [113.5439, 22.1987],
  Macau: [113.5439, 22.1987],
};

const chinaOutlineCoords: Array<[number, number]> = [
  [80, 43],
  [87, 48],
  [96, 43],
  [104, 45],
  [112, 41],
  [121, 40],
  [127, 47],
  [133, 44],
  [124, 38],
  [122, 31],
  [119, 25],
  [113, 22],
  [109, 18],
  [104, 22],
  [98, 21],
  [91, 28],
  [85, 31],
  [78, 35],
  [80, 43],
];

const gridX = [120, 240, 360, 480, 600];
const gridY = [92, 184, 276, 368];

const orderedTimeline = computed(() =>
  [...(props.timeline ?? [])].sort((left, right) => left.year - right.year),
);

const mappedPoints = computed(() =>
  orderedTimeline.value
    .map((entry) => {
      const location = normalizeLocation(entry.location || props.fallbackLocation || "");
      const coordinates = findCoordinates(location);
      if (!coordinates) {
        return null;
      }
      const [x, y] = project(coordinates);
      return {
        id: entry.id,
        year: entry.year,
        title: entry.title,
        location,
        x,
        y,
      };
    })
    .filter((point): point is NonNullable<typeof point> => Boolean(point)),
);

const unmappedEntries = computed(() =>
  orderedTimeline.value.filter((entry) => {
    const location = normalizeLocation(entry.location || "");
    return location && !findCoordinates(location);
  }),
);

const routePoints = computed(() => mappedPoints.value.map((point) => `${point.x},${point.y}`).join(" "));
const chinaOutlinePath = computed(() => {
  const points = chinaOutlineCoords.map((coord) => project(coord));
  return points.map(([x, y], index) => `${index === 0 ? "M" : "L"}${x},${y}`).join(" ") + " Z";
});

function findCoordinates(location: string) {
  if (!location) {
    return null;
  }

  const direct = cityCoordinates[location];
  if (direct) {
    return direct;
  }

  const match = Object.entries(cityCoordinates).find(([name]) => location.includes(name) || name.includes(location));
  return match?.[1] ?? null;
}

function normalizeLocation(value: string) {
  return value.replace(/市|省|自治区|特别行政区|,.*$/g, "").trim();
}

function project([longitude, latitude]: [number, number]) {
  const x = ((longitude - 73) / (135 - 73)) * 664 + 28;
  const y = ((54 - latitude) / (54 - 18)) * 404 + 28;
  return [Number(x.toFixed(1)), Number(y.toFixed(1))] as const;
}
</script>
