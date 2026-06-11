import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Branch } from '../../core/models/branch.model';
import { BranchService } from '../../core/services/branch.service';
import { AppHeader } from '../../shared/components/app-header/app-header';
import { AppButton } from '../../shared/components/app-button/app-button';

@Component({
  selector: 'app-branch-selection',
  imports: [
    CommonModule,
    AppHeader,
    AppButton
  ],
  templateUrl: './branch-selection.html',
  styleUrl: './branch-selection.scss',
})

export class BranchSelection implements OnInit {

  private branchService = inject(BranchService);
  private cdr = inject(ChangeDetectorRef);
  private router = inject(Router);

  branches: Branch[] = [];
  errorMessage = '';

  selectBranch(branchId: number) {

    localStorage.setItem(
      'selectedBranchId',
      branchId.toString()
    );

    this.router.navigate(['/services']);
  }

  ngOnInit(): void {
    this.branchService.getBranches()
      .subscribe({
        next: (data) => {
          this.branches = data;
          this.cdr.detectChanges();
          console.log(this.branches);
        },
        error: (err) => {
          this.errorMessage = 'Không tải được danh sách chi nhánh';
          this.cdr.detectChanges();
          console.error(err);
        }
      });
  }
}