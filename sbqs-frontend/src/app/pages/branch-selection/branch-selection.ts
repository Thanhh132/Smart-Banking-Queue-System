import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';

import { Branch } from '../../core/models/branch.model';
import { BranchService } from '../../core/services/branch.service';

@Component({
  selector: 'app-branch-selection',
  imports: [CommonModule],
  templateUrl: './branch-selection.html',
  styleUrl: './branch-selection.scss',
})
export class BranchSelection implements OnInit {

  private branchService = inject(BranchService);
  private cdr = inject(ChangeDetectorRef);

  branches: Branch[] = [];
  errorMessage = '';

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