import {
  ChangeDetectorRef,
  Component,
  inject,
  OnInit
} from '@angular/core';

import { CommonModule } from '@angular/common';

import { AppHeader } from '../../shared/components/app-header/app-header';
import { AppCard } from '../../shared/components/app-card/app-card';

import {
  QueueMonitor
} from '../../core/models/queue-monitor.model';

import {
  QueueMonitorService
} from '../../core/services/queue-monitor.service';

@Component({
  selector: 'app-queue-monitor',
  imports: [
    CommonModule,
    AppHeader,
    AppCard
  ],
  templateUrl: './queue-monitor.html',
  styleUrl: './queue-monitor.scss',
})
export class QueueMonitorComponent implements OnInit {

  private monitorService =
    inject(QueueMonitorService);

  private cdr =
    inject(ChangeDetectorRef);

  monitor: QueueMonitor | null = null;

  errorMessage = '';

  ngOnInit(): void {

    const branchId =
      localStorage.getItem(
        'selectedBranchId'
      );

    if (!branchId) {

      this.errorMessage =
        'Chưa chọn chi nhánh';

      return;
    }

    this.monitorService
      .getMonitor(Number(branchId))
      .subscribe({

        next: (data) => {

          this.monitor = data;

          this.cdr.detectChanges();

          console.log(data);
        },

        error: (err) => {

          this.errorMessage =
            'Không tải được dữ liệu monitor';

          this.cdr.detectChanges();

          console.error(err);
        }
      });
  }
}