import { Routes } from '@angular/router';
import { LoginComponent } from './login/login';
import { RegisterComponent } from './register/register';
import { HomeComponent } from './home/home'; 
// 1. IMPORTAMOS EL COMPONENTE DE PAGO
import { PaymentComponent } from './payment/payment.component'; 
import { RecoverPasswordComponent } from './recover-password/recover-password';
import { ResetPasswordComponent } from './reset-password/reset-password';
import { ConfirmAccountComponent } from './confirm-account/confirm-account';

export const routes: Routes = [
  // Si la ruta es 'login', mostramos el LoginComponent
  { path: 'login', component: LoginComponent },
  
  // Si la ruta es 'register', mostramos el RegisterComponent
  { path: 'register', component: RegisterComponent },
  
  { path: 'home', component: HomeComponent }, 

  // 2. AÑADIMOS LA RUTA DE PAGO
  { path: 'payment', component: PaymentComponent },

  // Recuperación de contraseña
  { path: 'recuperar-clave', component: RecoverPasswordComponent },
  { path: 'cambiar-clave', component: ResetPasswordComponent },

  // Confirmación de cuenta (desde email)
  { path: 'confirmar', component: ConfirmAccountComponent },

  // Si la ruta está vacía (al entrar), redirigimos al login automáticamente
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: '**', redirectTo: '/login' }
];