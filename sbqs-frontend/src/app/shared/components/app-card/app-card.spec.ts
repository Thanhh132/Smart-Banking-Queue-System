import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AppCard } from './app-card';

describe('AppCard', () => {
  let component: AppCard;
  let fixture: ComponentFixture<AppCard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppCard],
    }).compileComponents();

    fixture = TestBed.createComponent(AppCard);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('renders an optional compact header', () => {
    fixture.componentRef.setInput('title', 'Hàng đợi');
    fixture.componentRef.setInput('subtitle', 'Cập nhật theo thời gian thực');
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('h2').textContent).toContain('Hàng đợi');
    expect(fixture.nativeElement.querySelector('.app-card__heading p').textContent)
      .toContain('Cập nhật theo thời gian thực');
  });
});
