import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AppTopbar } from './app-topbar';

describe('AppTopbar', () => {
  let component: AppTopbar;
  let fixture: ComponentFixture<AppTopbar>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppTopbar],
    }).compileComponents();

    fixture = TestBed.createComponent(AppTopbar);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
