package br.com.jdsb.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.jdsb.domain.Trigger;

public interface TriggerRepository extends JpaRepository<Trigger, Integer> {
	List<Trigger> findByNmTrigger(String nmTrigger);

}
