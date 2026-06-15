import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminBranchSetup } from './admin-branch-setup';

describe('AdminBranchSetup', () => {
  let component: AdminBranchSetup;
  let fixture: ComponentFixture<AdminBranchSetup>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminBranchSetup],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminBranchSetup);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
