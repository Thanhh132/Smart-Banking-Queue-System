import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

export interface GeocodeResult {
  formattedAddress: string;
  latitude: number;
  longitude: number;
  province: string;
  district: string;
  ward: string;
}

@Injectable({ providedIn: 'root' })
export class LocationService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8081/api/locations';

  geocode(address: string) {
    const params = new HttpParams().set('address', address);
    return this.http.get<GeocodeResult>(`${this.apiUrl}/geocode`, { params });
  }

  distanceInKm(fromLat: number, fromLng: number, toLat: number, toLng: number): number {
    const earthRadiusKm = 6371;
    const toRadians = (degree: number) => degree * Math.PI / 180;
    const latitudeDistance = toRadians(toLat - fromLat);
    const longitudeDistance = toRadians(toLng - fromLng);
    const value = Math.sin(latitudeDistance / 2) ** 2
      + Math.cos(toRadians(fromLat)) * Math.cos(toRadians(toLat))
      * Math.sin(longitudeDistance / 2) ** 2;
    return earthRadiusKm * 2 * Math.atan2(Math.sqrt(value), Math.sqrt(1 - value));
  }

  googleMapsUrl(branch: { latitude?: number; longitude?: number; address?: string; branchName?: string }): string {
    if (branch.latitude != null && branch.longitude != null) {
      return `https://www.google.com/maps?q=${branch.latitude},${branch.longitude}&z=18`;
    }

    const query = `${branch.branchName || ''} ${branch.address || ''}`.trim();
    return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(query)}`;
  }
}
