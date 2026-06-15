import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminMappings } from './admin-mappings';

describe('AdminMappings', () => {
  let component: AdminMappings;
  let fixture: ComponentFixture<AdminMappings>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminMappings],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminMappings);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
