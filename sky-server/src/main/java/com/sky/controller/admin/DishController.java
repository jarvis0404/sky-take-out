package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Delete;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@Api(tags = "dish apis")
@RestController
@Slf4j
@RequestMapping("/admin/dish")
public class DishController {

    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;

    @ApiOperation(value = "add new dish")
    @PostMapping
    public Result save(@RequestBody DishDTO dishDTO) {
        dishService.saveWithFlavors(dishDTO);

        Long categoryId = dishDTO.getCategoryId();
        cleanRedisCache("dish_" + categoryId);

        return Result.success();
    }

    @ApiOperation(value = "page query")
    @GetMapping("/page")
    public Result<PageResult> pageQuery(DishPageQueryDTO dishQueryQueryDTO) {
        PageResult pageResult = dishService.pageQuery(dishQueryQueryDTO);
        return Result.success(pageResult);
    }

    @ApiOperation(value = "batch delete")
    @DeleteMapping
    public Result delete(@RequestParam List<Long> ids) {
        dishService.deleteBatch(ids);
        cleanRedisCache("dish_*");
        return Result.success();
    }

    /**
     *
     * @param id dish id
     * @return dish view object
     */
    @GetMapping("/{id}")
    public Result<DishVO> getById(@PathVariable Long id) {
        DishVO dishVO = dishService.getByIdWithFlavors(id);
        return Result.success(dishVO);
    }

    @PutMapping
    public Result update(@RequestBody DishDTO dishDTO) {
        dishService.updateWithFlavors(dishDTO);
        cleanRedisCache("dish_*");
        return Result.success();
    }

    @GetMapping("/list")
    public Result<List<Dish>> list(Long categoryId) {
        List<Dish> lst = dishService.listByCategoryId(categoryId);
        return Result.success(lst);
    }

    @PostMapping("/status/{status}")
    public Result updateStatus(@PathVariable Integer status, Long id) {
        dishService.updateStatus(status, id);
        cleanRedisCache("dish_*");
        return Result.success();
    }

    /**
     * clean redis cache based on redis key pattern
     * @param keyPattern key pattern
     */
    private void cleanRedisCache(String keyPattern) {
        Set keys = redisTemplate.keys(keyPattern);
        redisTemplate.delete(keys);
    }

}
