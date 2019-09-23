package com.team.TeamUp.utils;

import com.team.TeamUp.domain.Project;
import com.team.TeamUp.domain.Task;
import com.team.TeamUp.domain.User;
import com.team.TeamUp.domain.UserEvent;
import com.team.TeamUp.domain.enums.UserEventType;
import com.team.TeamUp.domain.enums.UserStatus;
import com.team.TeamUp.dtos.UserDTO;
import com.team.TeamUp.persistence.ProjectRepository;
import com.team.TeamUp.persistence.TaskRepository;
import com.team.TeamUp.persistence.UserEventRepository;
import com.team.TeamUp.persistence.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class UserUtils {

    private final UserRepository userRepository;
    private UserEventRepository userEventRepository;
    private TaskRepository taskRepository;
    private ProjectRepository projectRepository;
    private DTOsConverter dtOsConverter;

    @Autowired
    public UserUtils(UserRepository userRepository, UserEventRepository userEventRepository, TaskRepository taskRepository,
                     ProjectRepository projectRepository, DTOsConverter dtOsConverter) {
        this.userRepository = userRepository;
        this.userEventRepository = userEventRepository;
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.dtOsConverter = dtOsConverter;
    }

    public String decryptPassword(String password) {
        // TODO
        String key = "aesEncryptionKey";
//        String initVector = "encryptionIntVec";

        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");

            Key key1 =  new SecretKeySpec(key.getBytes(), "AES");
//            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes());
            byte[] initVector = new byte[cipher.getBlockSize()];
            IvParameterSpec iv = new IvParameterSpec(initVector);

            cipher.init(Cipher.DECRYPT_MODE, key1, iv);
            byte[] original = cipher.doFinal(Base64.decodeBase64(password));

            return new String(original);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public void createEvent(User creator, String description, UserEventType eventType){
        log.info(String.format("Entered method to create event with user %s, description %s and type %s", creator, description, eventType));
        UserEvent userEvent = new UserEvent();
        userEvent.setCreator(creator);
        userEvent.setDescription(description);
        userEvent.setEventType(eventType);
        userEvent.setTime(LocalDateTime.now());

        userEvent = userEventRepository.save(userEvent);
        List<UserEvent> history = creator.getHistory();
        if(history == null){
            history = new ArrayList<>();
        }
        history.add(userEvent);
        creator.setHistory(history);
        userRepository.save(creator);
        log.info("Event successfully created");
    }

    public void deleteUserInitiated(User user) {
        List<Task> assignedTasks = taskRepository.findAllByAssigneesContaining(user);
        List<Task> reportedTasks = taskRepository.findAllByReporter(user);
        List<Project> projects = projectRepository.findAllByOwner(user);
        User leader = user.getTeam().getLeader();

        for (Task task : assignedTasks) { //remove the user from assignees from all the tasks
            task.getAssignees().remove(user);
            taskRepository.save(task);
        }
        if (leader != null) { //if he has a leader, leader is now the reporter
            for (Task task : reportedTasks){
                task.setReporter(leader);
                taskRepository.save(task);
            }
        }else{
            List<User> admins = userRepository.findAllByStatus(UserStatus.ADMIN);
            leader = admins.size() > 0 ? admins.get(0) : createAdmin() ; // if he doesn't have a leader, set an admin. There should always be an admin, otherwise an error will be thrown
            for (Task task : reportedTasks) {
                task.setReporter(leader);
                taskRepository.save(task);
            }
        }

        // at this point the leader variable is already initialized with an existing user or a newly created
        // so the projects will be set to that user
        for (Project project : projects){
            project.setOwner(leader);
            projectRepository.save(project);
        }
    }

    private User createAdmin(){
        UserDTO userDTO = new UserDTO();
        userDTO.setFirstName("System");
        userDTO.setLastName("Administrator");
        int nano = LocalDateTime.now().getNano();
        userDTO.setUsername("admin" + nano);
        userDTO.setPassword(String.valueOf(nano));
        userDTO.setStatus(UserStatus.ADMIN);

        User user = dtOsConverter.getUserFromDTO(userDTO, UserStatus.ADMIN);
        user = userRepository.save(user);
        return user;
    }
}
