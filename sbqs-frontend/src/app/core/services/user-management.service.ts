import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { API_BASE_URL } from '../config/api.config';

@Injectable({
  providedIn: 'root'
})
export class UserManagementService {

  private http = inject(HttpClient);

  private apiUrl = `${inject(API_BASE_URL)}/users`;

  getUsersByBranch(branchId: number) {
    return this.http.get<any[]>(
      `${this.apiUrl}?branchId=${branchId}`
    );
  }

  getUsersByRole(role: string) {
    return this.http.get<any[]>(
      `${this.apiUrl}?role=${role}`
    );
  }

  createStaff(staff: any) {
    return this.http.post(
      `${this.apiUrl}/staff`,
      staff
    );
  }

  createAdminBranch(adminBranch: any) {
    return this.http.post(
      `${this.apiUrl}/admin-branch`,
      adminBranch
    );
  }

  deleteUser(userId: number) {
    return this.http.delete(`${this.apiUrl}/${userId}`);
  }

  updateUser(userId: number, payload: any) {
    return this.http.put(`${this.apiUrl}/${userId}`, payload);
  }

}
