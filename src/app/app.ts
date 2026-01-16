import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
// Las líneas de import del Login y Register se borran porque ya no hacen falta aquí

@Component({
  selector: 'app-root',
  standalone: true,
  // Limpiamos la lista de imports:
  imports: [RouterOutlet], 
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class AppComponent {
  title = 'gramolafe';
}