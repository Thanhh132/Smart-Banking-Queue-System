import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BranchSelection } from './branch-selection';

describe('BranchSelection', () => {
  let component: BranchSelection;
  let fixture: ComponentFixture<BranchSelection>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BranchSelection],
    }).compileComponents();

    fixture = TestBed.createComponent(BranchSelection);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
