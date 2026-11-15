import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-ayuda',
  imports: [RouterLink],
  template: `
    <article class="pagina-texto animate__animated animate__fadeIn">
      <h1 class="titulo-seccion">Ayuda</h1>

      <h2>¿Qué es Toca?</h2>
      <p>
        <strong>Toca — ¿Qué toca hoy?</strong> te ayuda a decidir qué cocinar teniendo en cuenta lo
        que ya has comido esta semana. Publicas recetas, registras las que consumes y la aplicación
        te recomienda las que encajan con las raciones semanales de cada grupo de alimentos.
      </p>

      <h2>¿Cómo funcionan las recomendaciones?</h2>
      <p>
        Cada grupo de alimentos (carne, pescado, legumbres, dulces…) tiene un número máximo de
        raciones por semana. Toca cuenta las recetas que has consumido en los
        <strong>últimos 7 días</strong> y, si un grupo ya ha llegado a su límite, deja de
        recomendarte las recetas que lo contienen. En «Mi alimentación» verás un círculo por grupo:
        verde si vas bien, amarillo si te acercas al límite y rojo si ya lo has alcanzado.
      </p>

      <h2>Primeros pasos</h2>
      <ol>
        <li>
          <a routerLink="/registro">Crea una cuenta</a> e inicia sesión. Si está activado, también
          puedes entrar con tu cuenta de Google.
        </li>
        <li>Busca recetas en «Recetas» o publica las tuyas desde «Mis recetas».</li>
        <li>En el detalle de una receta, pulsa «Añadir a consumidos» cuando la comas.</li>
        <li>Consulta «Qué toca hoy» para ver lo que te recomendamos.</li>
        <li>Organiza la semana en el «Planificador», en el menú con tu nombre.</li>
      </ol>

      <h2>He olvidado la contraseña</h2>
      <p>
        Usa <a routerLink="/olvido-password">«He olvidado la contraseña»</a> en la pantalla de
        inicio de sesión. Te enviaremos un enlace al email de tu cuenta para elegir una nueva. Si
        creaste la cuenta con Google y quieres tener también una contraseña, pide el enlace desde
        «Mi perfil».
      </p>
    </article>
  `,
})
export class Ayuda {}
