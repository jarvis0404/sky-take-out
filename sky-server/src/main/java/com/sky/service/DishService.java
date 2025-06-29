package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

import java.util.List;

public interface DishService {

    public void saveWithFlavors(DishDTO dishDTO);

    PageResult pageQuery(DishPageQueryDTO dishQueryQueryDTO);

    void deleteBatch(List<Long> ids);

    DishVO getByIdWithFlavors(Long id);

    void updateWithFlavors(DishDTO dishDTO);

    List<DishVO> listWithFlavor(Dish dish);

    List<Dish> listByCategoryId(Long categoryId);

    void updateStatus(Integer status, Long id);
}
