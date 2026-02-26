# ConvertEntity 模块模板和最佳实践指南

## 🎯 目标
为ConvertEntity模块提供完整的模板和最佳实践，方便快速添加新的数据转换方法。

## 📋 快速添加新转换方法的标准步骤

### 第一步：确定转换类型
假设我们要添加一个保存和获取**用户收藏**的功能：
- 网络数据：`UserFavorite` (包含id, userId, itemId, itemType, createTime)
- 数据库实体：`UserFavoriteEntity` (包含favoriteId, userId, itemId, itemType, createTime)

### 第二步：添加保存方法（使用模板）

```java
/**
 * 保存用户收藏数据到数据库
 * 
 * @param favorites 用户收藏列表
 * @return 是否保存成功
 */
public static boolean saveUserFavorites(List<UserFavorite> favorites) {
    // 1. 参数验证
    if (favorites == null || favorites.isEmpty()) {
        EasyLog.print(TAG, "用户收藏列表为空，跳过保存");
        return false;
    }
    
    // 2. 使用统一的数据库操作包装
    return executeDatabaseOperation(() -> {
        // 3. 删除旧数据
        DbService.getInstance().mUserFavoriteService.deleteAll();
        
        int successCount = 0;
        
        // 4. 批量转换和保存
        for (UserFavorite favorite : favorites) {
            if (favorite == null) {
                continue;
            }
            
            // 5. 使用专门的转换方法
            UserFavoriteEntity entity = convertUserFavoriteToEntity(favorite);
            if (entity != null) {
                DbService.getInstance().mUserFavoriteService.addEntity(entity);
                successCount++;
            }
        }
        
        // 6. 详细的日志记录
        EasyLog.print(TAG, "成功保存 " + successCount + "/" + favorites.size() + " 个用户收藏数据");
        return successCount > 0;
        
    }, "保存用户收藏数据");
}
```

### 第三步：添加获取方法（使用模板）

```java
/**
 * 获取用户收藏数据
 * 
 * @param userId 用户ID
 * @return 用户收藏列表，如果获取失败则返回空列表
 */
public static ArrayList<UserFavorite> getUserFavorites(int userId) {
    if (userId <= 0) {
        EasyLog.print(TAG, "用户ID无效: " + userId);
        return new ArrayList<>();
    }
    
    // 1. 使用统一的数据库查询包装
    ArrayList<UserFavoriteEntity> entities = executeDatabaseOperation(() ->
        DbService.getInstance().mUserFavoriteService.find(UserFavoriteEntityDao.Properties.UserId.eq(userId)),
        "获取用户" + userId + "的收藏数据"
    );
    
    if (entities == null || entities.isEmpty()) {
        EasyLog.print(TAG, "用户" + userId + "的收藏数据为空");
        return new ArrayList<>();
    }
    
    // 2. 使用统一的转换包装
    return executeDatabaseOperation(() -> {
        ArrayList<UserFavorite> resultList = new ArrayList<>();
        
        for (UserFavoriteEntity entity : entities) {
            if (entity == null) {
                continue;
            }
            
            // 3. 使用专门的反向转换方法
            UserFavorite favorite = convertEntityToUserFavorite(entity);
            if (favorite != null) {
                resultList.add(favorite);
            }
        }
        
        // 4. 详细的统计日志
        EasyLog.print(TAG, "成功转换 " + resultList.size() + "/" + entities.size() + " 个用户收藏数据");
        return resultList;
        
    }, "转换用户收藏数据");
}
```

### 第四步：添加专用的转换方法

```java
/**
 * 将UserFavorite转换为UserFavoriteEntity
 * 
 * @param favorite 用户收藏对象
 * @return 转换后的实体，如果转换失败则返回null
 */
private static UserFavoriteEntity convertUserFavoriteToEntity(UserFavorite favorite) {
    if (favorite == null) {
        return null;
    }
    
    try {
        UserFavoriteEntity entity = new UserFavoriteEntity();
        entity.setFavoriteId(StringHelper.getUuid()); // 生成新的ID
        entity.setUserId(favorite.getUserId());
        entity.setItemId(favorite.getItemId());
        entity.setItemType(favorite.getItemType());
        entity.setCreateTime(favorite.getCreateTime());
        return entity;
    } catch (Exception e) {
        EasyLog.print(TAG, "转换用户收藏失败: " + e.getMessage());
        return null;
    }
}

/**
 * 将UserFavoriteEntity转换为UserFavorite
 * 
 * @param entity 用户收藏实体
 * @return 转换后的对象，如果转换失败则返回null
 */
private static UserFavorite convertEntityToUserFavorite(UserFavoriteEntity entity) {
    if (entity == null) {
        return null;
    }
    
    try {
        UserFavorite favorite = new UserFavorite();
        favorite.setId(entity.getFavoriteId());
        favorite.setUserId(entity.getUserId());
        favorite.setItemId(entity.getItemId());
        favorite.setItemType(entity.getItemType());
        favorite.setCreateTime(entity.getCreateTime());
        return favorite;
    } catch (Exception e) {
        EasyLog.print(TAG, "转换用户收藏实体失败: " + e.getMessage());
        return null;
    }
}
```

## 🛠️ 快速添加其他转换的简化模板

### 对于简单的数据转换（无加密需求）：

```java
/**
 * 快速添加：[功能描述]
 * 
 * @param [参数] [描述]
 * @return [返回描述]
 */
public static [返回类型] [方法名]([参数类型] [参数]) {
    // 1. 参数验证
    if ([参数验证条件]) {
        EasyLog.print(TAG, "[错误描述]");
        return [默认值];
    }
    
    // 2. 数据库操作
    return executeDatabaseOperation(() -> {
        // 3. 业务逻辑
        [数据库操作代码]
        
        // 4. 结果处理
        return [结果];
        
    }, "[操作名称]");
}
```

### 对于需要加密的数据转换：

```java
/**
 * 快速添加：[功能描述]（含加密）
 * 
 * @param [参数] [描述]
 * @return [返回描述]
 */
public static [返回类型] [方法名]([参数类型] [参数]) {
    // 1. 参数验证
    if ([参数验证条件]) {
        EasyLog.print(TAG, "[错误描述]");
        return [默认值];
    }
    
    // 2. 使用加密转换
    return executeDatabaseOperation(() -> {
        [目标实体] entity = new [目标实体]();
        entity.set[字段1]([源数据].get[字段1]());
        entity.set[字段2](encryptIfNotEmpty([源数据].get[字段2]())); // 需要加密的字段
        entity.set[字段3]([源数据].get[字段3]());
        
        // 3. 保存到数据库
        DbService.getInstance().m[服务名]Service.addEntity(entity);
        
        return [结果];
        
    }, "[操作名称]");
}
```

## 📊 优化效果对比

### 优化前（传统写法）：
```java
public static void saveData(List<Data> list) {
    DbService.getInstance().mDataService.deleteAll();
    for (Data data : list) {
        Entity entity = new Entity();
        entity.setName(data.getName());
        entity.setValue(data.getValue());
        DbService.getInstance().mEntityService.addEntity(entity);
    }
}
```

### 优化后（使用工具方法）：
```java
public static boolean saveData(List<Data> list) {
    if (list == null || list.isEmpty()) {
        EasyLog.print(TAG, "数据列表为空，跳过保存");
        return false;
    }
    
    return executeDatabaseOperation(() -> {
        DbService.getInstance().mDataService.deleteAll();
        
        int successCount = 0;
        for (Data data : list) {
            Entity entity = convertDataToEntity(data);
            if (entity != null) {
                DbService.getInstance().mEntityService.addEntity(entity);
                successCount++;
            }
        }
        
        EasyLog.print(TAG, "成功保存 " + successCount + "/" + list.size() + " 个数据");
        return successCount > 0;
        
    }, "保存数据");
}
```

## 🎯 关键优势

1. **一致的异常处理**：所有方法都使用统一的异常处理机制
2. **详细的日志记录**：每个操作都有完整的日志记录，便于调试
3. **参数验证**：所有方法都有完善的输入参数验证
4. **统计信息**：提供详细的操作成功/失败统计
5. **代码复用**：大量使用通用的工具方法，减少重复代码
6. **易于维护**：职责分离，每个方法功能单一，易于理解和修改

## ⚡ 快速检查清单

添加新的转换方法时，确保：
- ✅ 参数验证（空值、有效性检查）
- ✅ 使用 `executeDatabaseOperation` 包装数据库操作
- ✅ 使用 `encryptIfNotEmpty` / `decryptIfNotEmpty` 处理加密字段
- ✅ 使用 `splitStringToList` / `listToString` 处理列表字段
- ✅ 提供详细的日志记录（操作前后都有日志）
- ✅ 返回合理的默认值（空列表、false等）
- ✅ 添加完整的JavaDoc注释

## 💡 使用技巧

1. **复制粘贴模板**：直接使用上述模板，替换相关类名和字段名
2. **保持一致性**：所有新方法都遵循相同的模式和结构
3. **逐步完善**：可以先实现基本功能，再逐步添加异常处理和日志
4. **充分测试**：每个新方法都要进行充分的功能测试
5. **代码审查**：使用统一的代码审查标准，确保质量一致

## 📈 预期效果

使用这套标准化模板，您可以：
- ⚡ **5分钟内**完成新的转换方法
- 🛡️ **零错误**实现完善的异常处理
- 📖 **100%一致性**保持代码风格统一
- 🔧 **轻松维护**后续修改和扩展

现在您有了一套完整的工具，可以快速、标准化地在ConvertEntity中添加任何新的数据转换方法！🎉