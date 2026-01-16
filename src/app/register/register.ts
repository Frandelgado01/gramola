import { Component } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

// ---------------------------------------------------------------------------
// IMPORTANTE: Verifica que esta ruta es correcta.
// Significa: "Sal de la carpeta register (../) entra en services y busca user.service"
import { UserService } from '../services/user.service'; 
// ---------------------------------------------------------------------------

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [FormsModule, CommonModule, RouterLink],
  templateUrl: './register.html',
  styleUrls: ['./register.css']
})
export class RegisterComponent { 
  
  barName: string = ''; 
  email: string = '';
  pwd: string = '';
  pwd2: string = '';
  spotifyId: string = '';
  spotifySecret: string = '';
  showSpotifyConfig = false;
  showPassword = false;
  showPassword2 = false;
  showSpotifySecret = false;
  loading = false;
  errorMsg = '';
  successMsg = '';

  // Si aquí te sigue saliendo error rojo, lee la "Nota sobre la ruta" más abajo
  constructor(private userService: UserService, private router: Router) {}

  toggleSpotify() {
    this.showSpotifyConfig = !this.showSpotifyConfig;
  }

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  togglePassword2(): void {
    this.showPassword2 = !this.showPassword2;
  }

  toggleSpotifySecret(): void {
    this.showSpotifySecret = !this.showSpotifySecret;
  }

  register() {
    this.errorMsg = '';
    this.successMsg = '';

    if (this.pwd !== this.pwd2) {
      this.errorMsg = 'Las contraseñas no coinciden.';
      return;
    }

    if (!this.barName || this.barName.trim() === '') {
       this.errorMsg = 'El nombre del establecimiento es obligatorio.';
       return;
    }

     if (!this.email || !this.pwd) {
      this.errorMsg = 'Completa email y contraseña.';
      return;
     }

    const user = {
      email: this.email,
      barName: this.barName, // Enviamos el nombre del bar para evitar el error SQL
      pwd: this.pwd,
      spotifyId: this.spotifyId,
      spotifySecret: this.spotifySecret
    };

    console.log("Enviando usuario:", user); 

    this.loading = true;
    this.userService.register(user).subscribe({
      next: () => {
        this.successMsg = '✅ Registro correcto. Revisa tu email para confirmar la cuenta.';
        this.loading = false;
        setTimeout(() => this.router.navigate(['/login'], { queryParams: { registered: '1' } }), 900);
      },
      // CORRECCIÓN: Añadido ': any' para que no se queje TypeScript
      error: (err: any) => {
        console.error(err);
        const msg = err?.error ? (err.error.message || err.message) : err?.message;
        this.errorMsg = msg || 'Error en el registro.';
        this.loading = false;
      }
    });
  }
}