import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { UserService } from '../services/user.service';

@Component({
  selector: 'app-recover-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './recover-password.html',
  styleUrls: ['./recover-password.css']
})
export class RecoverPasswordComponent {
  email = '';
  loading = false;
  successMsg = '';
  errorMsg = '';

  constructor(private userService: UserService) {}

  solicitar() {
    if (!this.email) return;
    this.loading = true;
    this.successMsg = '';
    this.errorMsg = '';

    this.userService.requestPasswordReset(this.email).subscribe({
      next: (res: any) => {
        this.successMsg = res?.message || 'Si el correo existe, recibirás un email con instrucciones.';
        this.loading = false;
      },
      error: (err) => {
        const msg = err?.error?.message || err?.message || 'Error';
        this.errorMsg = msg;
        this.loading = false;
      }
    });
  }
}
