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

  it('keeps subtitle compatibility and prefers description', () => {
    fixture.componentRef.setInput('title', 'Quản lý hàng đợi');
    fixture.componentRef.setInput('subtitle', 'Nội dung cũ');
    fixture.componentRef.setInput('description', 'Mô tả chuẩn');
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('h1').textContent).toContain('Quản lý hàng đợi');
    expect(fixture.nativeElement.querySelector('p').textContent).toContain('Mô tả chuẩn');
  });
});
