package br.com.jdsb.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.jdsb.domain.Parametro;

public interface ParametroRepository extends JpaRepository<Parametro, Integer> {

}
