import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  // Asegúrate de que este puerto coincide con tu Backend (8080)
  private url = '/users'; 

  constructor(private http: HttpClient) { }

  // CAMBIO CLAVE: Ahora acepta un objeto 'user' completo
  register(user: any): Observable<any> {
    return this.http.post(`${this.url}/register`, user);
  }

  // El login sigue igual, enviando un objeto
  login(email: string, pwd: string): Observable<any> {
    return this.http.post(`${this.url}/login`, { email, pwd });
  }

  confirmAccount(token: string): Observable<any> {
    return this.http.get(`${this.url}/confirm/${encodeURIComponent(token)}`);
  }

  requestPasswordReset(email: string): Observable<any> {
    return this.http.post(`${this.url}/password-reset/request`, { email });
  }

  confirmPasswordReset(token: string, newPwd: string): Observable<any> {
    return this.http.post(`${this.url}/password-reset/confirm`, { token, newPwd });
  }
}