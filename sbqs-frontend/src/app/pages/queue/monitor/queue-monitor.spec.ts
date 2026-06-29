import { ComponentFixture, TestBed } from '@angular/core/testing';

import { QueueMonitorComponent } from './queue-monitor';

describe('QueueMonitor', () => {
  let component: QueueMonitorComponent;
  let fixture: ComponentFixture<QueueMonitorComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [QueueMonitorComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(QueueMonitorComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
