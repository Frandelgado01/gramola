import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { UserService } from '../services/user.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './reset-password.html',
  styleUrls: ['./reset-password.css']
})
export class ResetPasswordComponent implements OnInit {
  token: string | null = null;
  newPwd = '';
  confirmPwd = '';
  loading = false;
  errorMsg = '';

  constructor(private route: ActivatedRoute, private router: Router, private userService: UserService) {}

  ngOnInit(): void {
    this.route.queryParamMap.subscribe((params) => {
      this.token = params.get('token');
    });
  }

  cambiar(): void {
    this.errorMsg = '';
    if (!this.token) {
      this.errorMsg = 'Falta el token en el enlace.';
      return;
    }
    if (!this.newPwd) return;
    if (this.newPwd !== this.confirmPwd) {
      this.errorMsg = 'Las contraseñas no coinciden.';
      return;
    }

    this.loading = true;
    this.userService.confirmPasswordReset(this.token, this.newPwd).subscribe({
      next: (res: any) => {
        this.loading = false;
        this.router.navigate(['/login'], { queryParams: { reset: '1' } });
      },
      error: (err) => {
        const msg = err?.error?.message || err?.message || 'Error';
        this.errorMsg = msg;
        this.loading = false;
      }
    });
  }
}
