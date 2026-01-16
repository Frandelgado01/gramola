import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class PaymentService {
  // Ajusta el puerto si tu backend no es el 8080
  private apiUrl = '/gramola/payments';
  private usersUrl = '/users';

  constructor(private http: HttpClient) { }

  // AHORA RECIBE EL TIPO DE PAGO (monthly, annual, song)
  prepay(email: string, paymentType: string): Observable<any> {
    const body = { email: email, paymentType: paymentType };
    return this.http.post(`${this.apiUrl}/prepay`, body);
  }

  // NUEVO: Avisar al backend que la suscripción se pagó para activar al usuario
  activateSubscription(email: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/activateSubscription`, { email });
  }
  
  // NUEVO: Obtener precios de suscripción desde la base de datos
  getSubscriptionPrices(): Observable<any[]> {
     return this.http.get<any[]>(`${this.usersUrl}/subscription-prices`);
  }
}