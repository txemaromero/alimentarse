package es.alimentarse.catalogo;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/** Tablas de catálogo: {@code grupo_ingrediente} y {@code tipo_receta}. SQL en {@code mybatis/CatalogoMapper.xml}. */
@Mapper
public interface CatalogoMapper {

    List<GrupoDto> grupos();

    List<TipoRecetaDto> tiposReceta();

    boolean existeGrupo(int id);

    boolean existeTipoReceta(int id);
}
