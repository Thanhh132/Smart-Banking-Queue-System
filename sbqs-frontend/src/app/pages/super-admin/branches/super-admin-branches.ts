import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { Branch } from '../../../core/models/branch.model';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { BranchService } from '../../../core/services/branch.service';
import { DashboardLayout } from '../../../shared/layouts/dashboard-layout/dashboard-layout';

@Component({
  selector: 'app-super-admin-branches',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, DashboardLayout],
  templateUrl: './super-admin-branches.html',
  styleUrl: './super-admin-branches.scss',
})
export class SuperAdminBranches implements OnInit {
  private fb = inject(FormBuilder);
  private branchService = inject(BranchService);
  private apiError = inject(ApiErrorService);
  private cdr = inject(ChangeDetectorRef);

  branches: Branch[] = [];
  isLoading = false;
  isSubmitting = false;
  isEditMode = false;
  editingBranchId: number | null = null;
  successMessage = '';
  errorMessage = '';

  bankOptions = [
    { label: 'BIDV', value: 'BIDV', code: 'BIDV' },
    { label: 'Vietcombank', value: 'Vietcombank', code: 'VCB' },
    { label: 'VietinBank', value: 'VietinBank', code: 'VTB' },
    { label: 'Agribank', value: 'Agribank', code: 'AGB' },
    { label: 'Techcombank', value: 'Techcombank', code: 'TCB' },
    { label: 'MB Bank', value: 'MB Bank', code: 'MB' },
    { label: 'ACB', value: 'ACB', code: 'ACB' },
    { label: 'Sacombank', value: 'Sacombank', code: 'STB' },
    { label: 'VPBank', value: 'VPBank', code: 'VPB' },
  ];

  provinceOptions = [
    'Binh Duong',
    'Ho Chi Minh',
    'Dong Nai',
    'Ba Ria - Vung Tau',
    'Long An',
    'Tay Ninh',
    'Can Tho',
    'Da Nang',
    'Ha Noi',
  ];

  districtOptions = [
    { label: 'Thu Dau Mot', value: 'Thu Dau Mot', code: 'TDM' },
    { label: 'Ben Cat', value: 'Ben Cat', code: 'BC' },
    { label: 'Di An', value: 'Di An', code: 'DA' },
    { label: 'Thuan An', value: 'Thuan An', code: 'TA' },
    { label: 'Tan Uyen', value: 'Tan Uyen', code: 'TU' },
    { label: 'Bau Bang', value: 'Bau Bang', code: 'BB' },
    { label: 'Bac Tan Uyen', value: 'Bac Tan Uyen', code: 'BTU' },
    { label: 'Phu Giao', value: 'Phu Giao', code: 'PG' },
    { label: 'Dau Tieng', value: 'Dau Tieng', code: 'DT' },
    { label: 'Quan 1', value: 'Quan 1', code: 'Q1' },
    { label: 'Quan 3', value: 'Quan 3', code: 'Q3' },
    { label: 'Quan 7', value: 'Quan 7', code: 'Q7' },
    { label: 'Thu Duc', value: 'Thu Duc', code: 'TD' },
    { label: 'Bien Hoa', value: 'Bien Hoa', code: 'BH' },
    { label: 'Vung Tau', value: 'Vung Tau', code: 'VT' },
    { label: 'Ninh Kieu', value: 'Ninh Kieu', code: 'NK' },
    { label: 'Hai Chau', value: 'Hai Chau', code: 'HC' },
    { label: 'Hoan Kiem', value: 'Hoan Kiem', code: 'HK' },
    { label: 'Cau Giay', value: 'Cau Giay', code: 'CG' },
  ];

  branchForm = this.fb.group({
    bankName: ['BIDV', [Validators.required]],
    branchCode: [''],
    branchName: ['', [Validators.required]],
    province: ['Binh Duong', [Validators.required]],
    district: ['Thu Dau Mot', [Validators.required]],
    address: ['', [Validators.required]],
    phone: ['', [Validators.required]],
    latitude: [10.7769, [Validators.required]],
    longitude: [106.7009, [Validators.required]],
    status: ['ACTIVE', [Validators.required]],
  });

  ngOnInit(): void {
    this.syncGeneratedBranchFields();
    this.branchForm.get('bankName')?.valueChanges.subscribe(() => this.syncGeneratedBranchFields());
    this.branchForm.get('district')?.valueChanges.subscribe(() => this.syncGeneratedBranchFields());
    this.loadBranches();
  }

  loadBranches(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.branchService.getBranches().subscribe({
      next: (branches) => {
        this.branches = branches || [];
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Load branches error:', err);
        this.errorMessage = this.apiError.getMessage(err, 'Khong tai duoc danh sach chi nhanh.');
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  submitBranch(): void {
    if (this.branchForm.invalid) {
      this.branchForm.markAllAsTouched();
      return;
    }

    const payload = this.branchForm.getRawValue();
    this.isSubmitting = true;
    this.successMessage = '';
    this.errorMessage = '';

    const request$ =
      this.isEditMode && this.editingBranchId
        ? this.branchService.updateBranch(this.editingBranchId, payload)
        : this.branchService.createBranch(payload);

    request$.subscribe({
      next: () => {
        this.successMessage = this.isEditMode
          ? 'Branch updated successfully.'
          : 'Branch created successfully.';
        this.isSubmitting = false;
        this.cancelEdit();
        this.loadBranches();
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Save branch error:', err);
        this.errorMessage = this.apiError.getMessage(err, 'Khong luu duoc chi nhanh.');
        this.isSubmitting = false;
        this.cdr.detectChanges();
      },
    });
  }

  startEdit(branch: Branch): void {
    this.isEditMode = true;
    this.editingBranchId = branch.branchId;
    this.branchForm.patchValue({
      bankName: branch.bankName,
      branchCode: branch.branchCode,
      branchName: branch.branchName,
      province: branch.province || 'Binh Duong',
      district: branch.district || 'Thu Dau Mot',
      address: branch.address,
      phone: branch.phone,
      latitude: branch.latitude ?? 10.7769,
      longitude: branch.longitude ?? 106.7009,
      status: branch.status || 'ACTIVE',
    });
  }

  deleteBranch(branch: Branch): void {
    const confirmed = confirm(
      `Xoa han chi nhanh "${branch.branchName}"? Neu chi nhanh dang co user/ticket, he thong se khong cho xoa.`
    );

    if (!confirmed) {
      return;
    }

    this.successMessage = '';
    this.errorMessage = '';

    this.branchService.deleteBranch(branch.branchId).subscribe({
      next: () => {
        this.successMessage = 'Da xoa chi nhanh.';
        this.loadBranches();
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Delete branch error:', err);
        this.errorMessage = this.apiError.getMessage(
          err,
          'Khong xoa duoc chi nhanh. Co the chi nhanh dang duoc su dung.'
        );
        this.cdr.detectChanges();
      },
    });
  }

  cancelEdit(): void {
    this.isEditMode = false;
    this.editingBranchId = null;
    this.branchForm.reset({
      bankName: 'BIDV',
      branchCode: '',
      branchName: '',
      province: 'Binh Duong',
      district: 'Thu Dau Mot',
      address: '',
      phone: '',
      latitude: 10.7769,
      longitude: 106.7009,
      status: 'ACTIVE',
    });
    this.syncGeneratedBranchFields();
    this.cdr.detectChanges();
  }

  private syncGeneratedBranchFields(): void {
    if (this.isEditMode) {
      return;
    }

    const bank = this.branchForm.get('bankName')?.value || 'BIDV';
    const district = this.branchForm.get('district')?.value || 'Thu Dau Mot';
    const bankOption = this.bankOptions.find((item) => item.value === bank);
    const districtOption = this.districtOptions.find((item) => item.value === district);

    this.branchForm.patchValue(
      {
        branchCode: `${bankOption?.code || 'BANK'}-${districtOption?.code || 'BR'}`,
        branchName: `${bank} ${district}`,
      },
      { emitEvent: false }
    );
  }
}
