import { loadTranslations } from '@angular/localize';
import { bootstrapApplication } from '@angular/platform-browser';
import { App } from './app/app';
import { appConfig } from './app/app.config';
import { TRADUCCIONES_NGB } from './locale/ngb-es';

loadTranslations(TRADUCCIONES_NGB);

bootstrapApplication(App, appConfig).catch((err) => console.error(err));
