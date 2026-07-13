import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, EnvironmentProviders, Provider } from '@angular/core';
import { provideRouter } from '@angular/router';

@Component({ template: '' })
class TestRouteComponent {}

export default [
  provideRouter([{ path: '**', component: TestRouteComponent }]),
  provideHttpClient(),
  provideHttpClientTesting(),
] satisfies Array<Provider | EnvironmentProviders>;
