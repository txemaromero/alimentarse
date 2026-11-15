import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { SwPush } from '@angular/service-worker';
import { BehaviorSubject } from 'rxjs';
import { AvisosPush, CLAVE_DUENO_AVISOS } from './avisos-push';

const ENDPOINT = 'https://fcm.googleapis.com/fcm/send/abc';
const CLAVE =
  'BP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A8';

function suscripcionFalsa(): PushSubscription {
  return {
    endpoint: ENDPOINT,
    toJSON: () => ({ endpoint: ENDPOINT, keys: { p256dh: 'BCVx', auth: 'BTBZ' } }),
  } as unknown as PushSubscription;
}

/** SwPush simulado: `subscription` cambia al suscribirse y al darse de baja, como el de verdad. */
function swFalso(habilitado = true, suscrito = false) {
  const subscription = new BehaviorSubject<PushSubscription | null>(
    suscrito ? suscripcionFalsa() : null,
  );
  return {
    isEnabled: habilitado,
    subscription,
    requestSubscription: vi.fn(async () => {
      const suscripcion = suscripcionFalsa();
      subscription.next(suscripcion);
      return suscripcion;
    }),
    unsubscribe: vi.fn(async () => subscription.next(null)),
  };
}

describe('AvisosPush', () => {
  let servidor: HttpTestingController;
  let sw: ReturnType<typeof swFalso>;
  const permiso = { permission: 'default' as NotificationPermission };
  const pendientes = () => new Promise((r) => setTimeout(r));

  function crear(falso = swFalso()) {
    sw = falso;
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: SwPush, useValue: sw },
      ],
    });
    servidor = TestBed.inject(HttpTestingController);
    return TestBed.inject(AvisosPush);
  }

  async function conClave(avisos: AvisosPush, clave: string | null = CLAVE) {
    const comprobacion = avisos.comprobar();
    servidor.expectOne('/api/push/clave').flush({ clavePublica: clave });
    await comprobacion;
  }

  beforeEach(() => {
    permiso.permission = 'default';
    vi.stubGlobal('Notification', permiso);
    localStorage.removeItem(CLAVE_DUENO_AVISOS);
  });

  afterEach(() => {
    servidor.verify();
    vi.unstubAllGlobals();
    localStorage.removeItem(CLAVE_DUENO_AVISOS);
  });

  it('sin service worker no hay avisos ni se pregunta al servidor', async () => {
    const avisos = crear(swFalso(false));
    await avisos.comprobar();
    expect(avisos.estado()).toBe('no-disponibles');
  });

  it('sin claves en el servidor tampoco', async () => {
    const avisos = crear();
    expect(avisos.estado()).toBe('comprobando');
    await conClave(avisos, null);
    expect(avisos.estado()).toBe('no-disponibles');
  });

  it('si no responde el servidor, no están disponibles', async () => {
    const avisos = crear();
    const comprobacion = avisos.comprobar();
    servidor.expectOne('/api/push/clave').flush(null, { status: 500, statusText: 'Error' });
    await comprobacion;
    expect(avisos.estado()).toBe('no-disponibles');
  });

  it('pregunta la clave una sola vez', async () => {
    const avisos = crear();
    await conClave(avisos);
    await avisos.comprobar();
    expect(avisos.estado()).toBe('desactivados');
  });

  it('al activarlos se suscribe con la clave del servidor y guarda la suscripción', async () => {
    const avisos = crear();
    await conClave(avisos);

    const activacion = avisos.activar('lucia');
    await pendientes();
    expect(sw.requestSubscription).toHaveBeenCalledWith({ serverPublicKey: CLAVE });
    const peticion = servidor.expectOne('/api/push/suscripcion');
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body).toEqual(
      expect.objectContaining({ endpoint: ENDPOINT, p256dh: 'BCVx', auth: 'BTBZ' }),
    );
    expect(typeof peticion.request.body.zona).toBe('string');
    peticion.flush(null);
    await activacion;

    expect(avisos.estado()).toBe('activados');
    expect(localStorage.getItem(CLAVE_DUENO_AVISOS)).toBe('lucia');
  });

  it('si el servidor no guarda la suscripción, este navegador tampoco queda suscrito (D-089)', async () => {
    const avisos = crear();
    await conClave(avisos);

    const activacion = avisos.activar('lucia');
    await pendientes();
    servidor.expectOne('/api/push/suscripcion').flush(
      {
        codigo: 'LIMITE_NAVEGADORES',
        mensaje: 'Ya tienes los avisos activados en 10 navegadores',
      },
      { status: 409, statusText: 'Conflict' },
    );

    await expect(activacion).rejects.toBeInstanceOf(HttpErrorResponse);
    expect(sw.unsubscribe).toHaveBeenCalled();
    expect(avisos.estado()).toBe('desactivados');
    expect(localStorage.getItem(CLAVE_DUENO_AVISOS)).toBeNull();
  });

  it('si el usuario deniega el permiso, quedan bloqueados', async () => {
    const avisos = crear();
    await conClave(avisos);
    sw.requestSubscription.mockImplementationOnce(async () => {
      permiso.permission = 'denied';
      throw new DOMException('Denegado', 'NotAllowedError');
    });

    await expect(avisos.activar('lucia')).rejects.toThrow();
    expect(avisos.estado()).toBe('bloqueados');
  });

  it('al desactivarlos borra la suscripción del servidor y de este navegador', async () => {
    const avisos = crear(swFalso(true, true));
    await conClave(avisos);
    localStorage.setItem(CLAVE_DUENO_AVISOS, 'lucia');
    expect(avisos.estado()).toBe('activados');

    const desactivacion = avisos.desactivar();
    const peticion = servidor.expectOne((r) => r.url === '/api/push/suscripcion');
    expect(peticion.request.method).toBe('DELETE');
    expect(peticion.request.params.get('endpoint')).toBe(ENDPOINT);
    peticion.flush(null);
    await desactivacion;

    expect(sw.unsubscribe).toHaveBeenCalled();
    expect(avisos.estado()).toBe('desactivados');
    expect(localStorage.getItem(CLAVE_DUENO_AVISOS)).toBeNull();
  });

  it('al cerrar sesión deja de recibirlos aunque falle el servidor', async () => {
    const avisos = crear(swFalso(true, true));

    const cierre = avisos.alCerrarSesion();
    servidor
      .expectOne((r) => r.url === '/api/push/suscripcion')
      .flush(null, { status: 500, statusText: 'Error' });
    await expect(cierre).resolves.toBeUndefined();
    expect(sw.unsubscribe).toHaveBeenCalled();
  });

  it('sin suscripción, desactivar no hace nada', async () => {
    const avisos = crear();
    await avisos.desactivar();
    expect(sw.unsubscribe).not.toHaveBeenCalled();
  });

  it('al iniciar sesión otra persona, los avisos de la anterior dejan de llegar', async () => {
    localStorage.setItem(CLAVE_DUENO_AVISOS, 'marcos');
    const avisos = crear(swFalso(true, true));

    await avisos.alIniciarSesion('lucia');

    expect(sw.unsubscribe).toHaveBeenCalled();
    expect(localStorage.getItem(CLAVE_DUENO_AVISOS)).toBeNull();
  });

  it('si vuelve a entrar la misma persona, sus avisos siguen', async () => {
    localStorage.setItem(CLAVE_DUENO_AVISOS, 'lucia');
    const avisos = crear(swFalso(true, true));

    await avisos.alIniciarSesion('lucia');
    expect(sw.unsubscribe).not.toHaveBeenCalled();
  });

  it('sin service worker, iniciar sesión no toca nada', async () => {
    localStorage.setItem(CLAVE_DUENO_AVISOS, 'marcos');
    const avisos = crear(swFalso(false, true));

    await avisos.alIniciarSesion('lucia');
    expect(sw.unsubscribe).not.toHaveBeenCalled();
  });

  it('el aviso de prueba lo pide al servidor', () => {
    const avisos = crear();
    avisos.probar().subscribe();
    servidor.expectOne('/api/push/prueba').flush({ enviados: 1 });
  });
});
