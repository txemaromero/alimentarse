package es.alimentarse.catalogo;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Catálogos para cualquier usuario. Los grupos y sus raciones se editan desde la administración
 * ({@code CatalogoAdminService}, D-065); los tipos de receta, con una migración.
 */
@Service
public class CatalogoService {

    private final CatalogoMapper catalogo;

    public CatalogoService(CatalogoMapper catalogo) {
        this.catalogo = catalogo;
    }

    @Transactional(readOnly = true)
    public List<GrupoDto> grupos() {
        return catalogo.grupos();
    }

    @Transactional(readOnly = true)
    public List<TipoRecetaDto> tiposReceta() {
        return catalogo.tiposReceta();
    }
}
