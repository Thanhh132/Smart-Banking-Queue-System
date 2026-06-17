import {
  ChangeDetectorRef,
  Component,
  OnInit,
  inject
} from '@angular/core';
import { CommonModule } from '@angular/common';

import { AppHeader } from '../../shared/components/app-header/app-header';
import { AppCard } from '../../shared/components/app-card/app-card';
import { AppButton } from '../../shared/components/app-button/app-button';
import { FormsModule } from '@angular/forms';

import { UserManagementService } from '../../core/services/user-management.service';

@Component({
  selector: 'app-admin-users',
  imports: [
    CommonModule,
    FormsModule,
    AppHeader,
    AppCard,
    AppButton
  ],
  templateUrl: './admin-users.html',
  styleUrl: './admin-users.scss'
})
export class AdminUsers implements OnInit {

  private userService = inject(UserManagementService);
  private cdr = inject(ChangeDetectorRef);

  users: any[] = [];
  branchId = 1;

  newStaff = {
    fullName: '',
    email: '',
    password: '',
    phone: '',
    branchId: 1
  };

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.userService
      .getUsersByBranch(this.branchId)
      .subscribe({
        next: (data: any[]) => {
          this.users = data;
          this.cdr.detectChanges();
        },
        error: (err: any) => {
          console.error(err);
        }
      });
  }

  createStaff(): void {
    this.userService
      .createStaff(this.newStaff)
      .subscribe({
        next: () => {
          this.loadUsers();
          this.newStaff = {
            fullName: '',
            email: '',
            password: '',
            phone: '',
            branchId: this.branchId
          };
          this.cdr.detectChanges();
        },
        error: (err: any) => {
          console.error(err);
        }
      });
  }
}
