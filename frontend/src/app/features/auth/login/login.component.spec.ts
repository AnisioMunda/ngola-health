import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
  let authService: {
    isLoggedIn: jasmine.Spy;
    login: jasmine.Spy;
  };
  let route: { snapshot: { queryParamMap: ReturnType<typeof convertToParamMap> } };
  let router: Router;

  beforeEach(() => {
    authService = {
      isLoggedIn: jasmine.createSpy('isLoggedIn').and.returnValue(false),
      login: jasmine.createSpy('login').and.returnValue(
        of({
          id: 'user-id',
          fullName: 'Utilizador de Teste',
          username: 'teste',
          email: 'teste@example.invalid',
          accessToken: 'access-token',
          refreshToken: 'refresh-token',
          roles: ['DOCTOR'],
          mustChangePassword: false,
        }),
      ),
    };
    route = {
      snapshot: { queryParamMap: convertToParamMap({ returnUrl: '/patients/42?tab=history' }) },
    };

    TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
        { provide: ActivatedRoute, useValue: route },
      ],
    });
    router = TestBed.inject(Router);
  });

  it('returns to the originally requested route after a successful login', () => {
    const navigateByUrl = spyOn(router, 'navigateByUrl').and.returnValue(Promise.resolve(true));
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();
    fixture.componentInstance.form.setValue({
      email: 'teste@example.invalid',
      password: 'senha-temporaria',
    });

    fixture.componentInstance.onSubmit();

    expect(authService.login).toHaveBeenCalledWith({
      email: 'teste@example.invalid',
      password: 'senha-temporaria',
    });
    expect(navigateByUrl).toHaveBeenCalledWith('/patients/42?tab=history');
  });

  it('does not navigate to an external return URL', () => {
    route.snapshot.queryParamMap = convertToParamMap({
      returnUrl: 'https://untrusted.example',
    });
    const navigateByUrl = spyOn(router, 'navigateByUrl').and.returnValue(Promise.resolve(true));
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();
    fixture.componentInstance.form.setValue({
      email: 'teste@example.invalid',
      password: 'senha-temporaria',
    });

    fixture.componentInstance.onSubmit();

    expect(navigateByUrl).toHaveBeenCalledWith('/dashboard');
  });
});
