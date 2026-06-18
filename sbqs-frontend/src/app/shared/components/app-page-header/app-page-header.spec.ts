import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AppPageHeader } from './app-page-header';

describe('AppPageHeader', () => {
  let component: AppPageHeader;
  let fixture: ComponentFixture<AppPageHeader>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppPageHeader],
    }).compileComponents();

    fixture = TestBed.createComponent(AppPageHeader);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
