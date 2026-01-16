import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
  
  private socket: WebSocket | undefined;
  
  // Usamos un Subject para "emitir" los mensajes a quien esté escuchando (el Home)
  // Es como una antena de radio: recibe del socket y emite a los componentes
  private messagesSubject = new Subject<any>();
  public messages$ = this.messagesSubject.asObservable();

  constructor() { }

  public connect(): void {
    // Conectamos con el endpoint que definimos en Java
    // OJO: Fíjate que usa el protocolo 'ws://' en vez de 'http://'
    const wsProtocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
    this.socket = new WebSocket(`${wsProtocol}://${window.location.host}/ws`);

    this.socket.onopen = () => {
      console.log('🔗 WebSocket Conectado');
    };

    this.socket.onmessage = (event) => {
      // Cuando llega un mensaje del servidor (Java), lo convertimos a JSON
      console.log('📩 Mensaje recibido:', event.data);
      const data = JSON.parse(event.data);
      
      // Y lo enviamos a todos los componentes suscritos
      this.messagesSubject.next(data);
    };

    this.socket.onclose = (event) => {
      console.log('🔌 WebSocket Desconectado', event);
    };

    this.socket.onerror = (error) => {
      console.error('❌ Error WebSocket:', error);
    };
  }
}