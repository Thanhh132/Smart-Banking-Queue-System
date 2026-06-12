import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminServices } from './admin-services';

describe('AdminServices', () => {
  let component: AdminServices;
  let fixture: ComponentFixture<AdminServices>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminServices],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminServices);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
