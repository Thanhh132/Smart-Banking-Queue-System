import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ServiceSelection } from './service-selection';

describe('ServiceSelection', () => {
  let component: ServiceSelection;
  let fixture: ComponentFixture<ServiceSelection>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ServiceSelection],
    }).compileComponents();

    fixture = TestBed.createComponent(ServiceSelection);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
