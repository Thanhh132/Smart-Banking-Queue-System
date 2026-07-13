import { InjectionToken } from '@angular/core';

/** Điểm cấu hình duy nhất cho toàn bộ API; có thể override khi triển khai. */
export const API_BASE_URL = new InjectionToken<string>('API_BASE_URL', {
  factory: () => 'http://localhost:8081/api',
});
