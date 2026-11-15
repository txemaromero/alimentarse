import { Component } from '@angular/core';

@Component({
  selector: 'app-informacion-legal',
  template: `
    <article class="pagina-texto animate__animated animate__fadeIn">
      <h1 class="titulo-seccion">Información legal</h1>

      <p class="text-body-secondary">
        Toca es un proyecto académico. Estas condiciones resumen cómo se usan tus datos y qué se
        espera de quien publica contenido.
      </p>

      <h2>Datos personales</h2>
      <p>
        Para crear una cuenta guardamos tu nombre de usuario, nombre, apellidos y email. La
        contraseña nunca se guarda tal cual, sino como un resumen (hash) irreversible. Tus consumos
        y tu planificador solo los ves tú y se usan únicamente para calcular tus recomendaciones y
        tus estadísticas. No cedemos tus datos a terceros.
      </p>
      <p>
        Si entras con Google, Google nos da tu email, tu nombre y un identificador de tu cuenta de
        Google, que guardamos para reconocerte la próxima vez. No conocemos tu contraseña de Google
        ni accedemos a nada más de tu cuenta.
      </p>
      <p>
        En «Mi perfil» puedes descargar en cualquier momento todos tus datos (un fichero JSON) y
        eliminar tu cuenta. Al eliminarla se borran tus datos, tus recetas, tus consumos, tu
        planificador y tus «me gusta». Si alguien ya había comido alguna de tus recetas, en su
        histórico se conservan solo el título y los ingredientes, sin tu nombre, para que sus
        cuentas de la semana sigan siendo correctas.
      </p>

      <h2>Contenido publicado</h2>
      <p>
        Al publicar una receta declaras que el texto y la imagen son tuyos o que puedes usarlos, y
        permites que el resto de usuarios de Toca los vean. Puedes dejar de publicarla o eliminarla
        en cualquier momento desde «Mis recetas». La administración puede ocultar el contenido que
        no sea adecuado, indicándote el motivo, y bloquear las cuentas que hagan un mal uso.
      </p>

      <h2>Información nutricional</h2>
      <p>
        Las raciones semanales recomendadas son orientativas y no sustituyen el consejo de un
        profesional de la salud o de la nutrición.
      </p>
      <p>
        La información nutricional de las recetas es una estimación orientativa, no una etiqueta
        nutricional. Se calcula con los valores de la
        <a href="https://www.bedca.net/" target="_blank" rel="noopener"
          >AESAN/BEDCA Base de Datos Española de Composición de Alimentos v1.0 (2010)<span
            class="visually-hidden"
          >
            (se abre en otra pestaña)</span
          ></a
        >, que se usan sin cambios, con fines educativos y citando su fuente.
      </p>

      <h2>Cookies y almacenamiento local</h2>
      <p>
        Toca no usa cookies de seguimiento. Solo usa dos cookies técnicas, necesarias para que la
        sesión funcione de forma segura: una con tu sesión, que se borra al cerrarla, y otra que
        protege tus acciones frente a otras webs. Tu nombre y tus permisos se guardan en el
        almacenamiento local del navegador para mostrar la interfaz, y también se borran al cerrar
        la sesión. Si entras con Google, se usan además, solo durante unos minutos, una cookie para
        el intercambio con Google y, si es la primera vez, otra para terminar de crear la cuenta.
      </p>
    </article>
  `,
})
export class InformacionLegal {}
