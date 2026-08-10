import { TestBed } from '@angular/core/testing';

import { AppDataTableShell } from './app-data-table-shell';

describe('AppDataTableShell', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [AppDataTableShell] }).compileComponents();
  });

  it('shows loading before empty state or projected table content', () => {
    const fixture = TestBed.createComponent(AppDataTableShell);
    fixture.componentRef.setInput('loading', true);
    fixture.componentRef.setInput('empty', true);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-loading-state')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('app-empty-state')).toBeFalsy();
  });

  it('shows the empty state when requested', () => {
    const fixture = TestBed.createComponent(AppDataTableShell);
    fixture.componentRef.setInput('empty', true);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-empty-state')).toBeTruthy();
  });
});
