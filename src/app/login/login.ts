import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms'; 
import { UserService } from '../services/user.service';
import { CommonModule } from '@angular/common'; 
import { ActivatedRoute, Router, RouterLink } from '@angular/router'; 

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, CommonModule, RouterLink], 
  templateUrl: './login.html',
  styleUrls: ['./login.css']
})
export class LoginComponent {
  email: string = '';
  pwd: string = '';
  loading = false;
  errorMsg = '';
  successMsg = '';
  infoMsg = '';
  showPassword = false;

  constructor(
    private userService: UserService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.route.queryParamMap.subscribe((params) => {
      const registered = params.get('registered');
      const reset = params.get('reset');
      const subscribed = params.get('subscribed');
      if (registered === '1') {
        this.successMsg = '✅ Registro correcto. Revisa tu email para confirmar la cuenta.';
      }
      if (reset === '1') {
        this.successMsg = '✅ Contraseña actualizada. Ya puedes iniciar sesión.';
      }
      if (subscribed === '1') {
        this.successMsg = '✅ Pago recibido. Inicia sesión para entrar al Home.';
      }
    });
  }

  login() {
    this.errorMsg = '';
    this.infoMsg = '';
    // keep any existing success banner

    if (!this.email || !this.pwd) {
      this.errorMsg = 'Completa email y contraseña.';
      return;
    }

    this.loading = true;
    this.userService.login(this.email, this.pwd).subscribe({
      next: (usuario: any) => {
        console.log('Login exitoso:', usuario);
        
        // 1. Guardamos al usuario en el navegador
        localStorage.setItem('usuario_gramola', JSON.stringify(usuario));

        // 2. NUEVA LÓGICA: Comprobamos si ha pagado la suscripción
        // El campo 'subscriptionActive' viene de tu backend Java (User.java)
        if (usuario.subscriptionActive) {
           // Si YA ha pagado -> Entra al bar (Home)
           this.loading = false;
           this.router.navigate(['/home']);
        } else {
           // Si NO ha pagado -> Le mandamos a pagar
           this.loading = false;
           this.router.navigate(['/payment'], { queryParams: { mode: 'subscription', welcome: '1' } });
        }
      },
      error: (err) => {
        // Manejo de error seguro por si err.error no existe
        const msg = err?.error ? (err.error.message || err.message) : 'Error de conexión';
        this.errorMsg = msg;
        this.loading = false;
      }
    });
  }

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }
}