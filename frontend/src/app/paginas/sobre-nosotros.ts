import { Component } from '@angular/core';

@Component({
  selector: 'app-sobre-nosotros',
  template: `
    <article class="pagina-texto animate__animated animate__fadeIn">
      <h1 class="titulo-seccion">Sobre nosotros</h1>
      <p>
        <strong>Alimentarse</strong> nace de una pregunta que nos hacemos casi todos los días:
        <em>¿qué como hoy?</em> Queríamos una forma sencilla de variar la dieta sin llevar la cuenta
        a mano de cuántas veces hemos comido carne, pescado o legumbres en la semana.
      </p>
      <p>
        Toca reúne recetas compartidas por sus usuarios y las cruza con tu historial de los últimos
        siete días para proponerte lo que te toca: platos que mantengan el equilibrio entre grupos
        de alimentos.
      </p>
      <p>
        Está desarrollada con Angular y Spring Boot. Las recetas y las imágenes de ejemplo son
        originales y se han creado para esta aplicación.
      </p>
    </article>
  `,
})
export class SobreNosotros {}
