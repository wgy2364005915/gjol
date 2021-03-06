package com.taokoo.www.service;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.taokoo.www.dao.mysql.LoginRecordDao;
import com.taokoo.www.dao.mysql.UserDao;
import com.taokoo.www.domain.StaticConfig;
import com.taokoo.www.domain.po.user.LoginRecord;
import com.taokoo.www.domain.po.user.User;
import com.taokoo.www.domain.vo.Result;
import com.taokoo.www.util.ImageUtil;
import com.taokoo.www.util.VailCodeUtil;

@Component
public class UserService {

	@Autowired
	private UserDao userDao;

	@Autowired
	private TokenService tokenService;

	@Autowired
	private LoginRecordDao loginRecordDao;
	
	@Autowired
	private StaticConfig staticConfig;
	
	public Result login(String username, String password) {
		try {
			List<User> lst = userDao.findByUsernameAndPassword(username, password);
			if (lst.size() > 0) {
				User user = lst.get(0);
				String token = tokenService.createToken(user);
				// 统计登录
				LoginRecord loginRecord = new LoginRecord();
				loginRecord.setTime(new Date());
				loginRecord.setUserId(user.getId());
				loginRecordDao.save(loginRecord);
				return Result.success(token);
			}
			return Result.fail("账号或密码不正确~");
		} catch (Exception e) {
			e.printStackTrace();
			return Result.fail("服务器异常");
		}
	}

	public Result sendMail(String mail) {
		try {
			Integer code = VailCodeUtil.getVailCode();
//TODO 验证码发送测试期间关闭			MailUtil.sendMail(mail, "您的验证码是：" + code + "，验证码3分钟内有效", "古剑奇谭网络版玩家社区注册验证码");
			tokenService.set(mail, code);//验证码存入缓存
			tokenService.expire(mail, 180);//设置3分钟过期时间
			return Result.success("验证码发送成功");
		} catch (Exception e) {
			e.printStackTrace();
			return Result.fail("服务器异常");
		}
	}
	
	@Transactional(rollbackFor = Exception.class)
	public Result register(String username,String password,String mail,Integer code) {
		List<User> lst = userDao.findByUsername(username);
		if(lst.size() > 0) return Result.fail("该账号已被注册");
		Integer vCode = tokenService.get(mail) == null ? 0 : (int) tokenService.get(mail);
		if(vCode.equals(0) || !vCode.equals(code))return Result.fail("验证码不正确");
		User user = new User();
		user.setUsername(username);
		user.setPassword(password);
		user.setMail(mail);
		//TODO
//		user.setType(1);//普通用户
//		user.setStatus(1);//账号状态正常
		user.setCreateTime(new Date());
		userDao.save(user);
		return Result.success("注册成功");
	}
	
	public Result getInfo(Integer userId) {
		User user = userDao.findOne(userId);
		if (null != user)return Result.success(user);
		return Result.fail("没有找到对应用户信息");
	}
	
	/**
	  * 用户设置头像
	 * @Title: setHeadPortrait  
	 * @param userId
	 * @param headPortrait 头像的base64码
	 * @return 头像url
	 * @author Taokoo
	 */
	public Result setHeadPortrait(Integer userId,String headPortrait) {
		User user = userDao.findOne(userId);
		if (null == user)return Result.fail("没有找到对应用户信息");
		String imgUrl = ImageUtil.saveHeadPortrait(headPortrait,user.getUsername(),staticConfig.getImgpath(),staticConfig.getImageserverpath());
		user.setHeadPortrait(imgUrl);
		userDao.save(user);
		return Result.success(imgUrl);
	}
	
}
