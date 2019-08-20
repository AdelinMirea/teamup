package com.team.TeamUp.utils;

import com.team.TeamUp.domain.Task;
import com.team.TeamUp.domain.User;
import com.team.TeamUp.dtos.TaskDTO;
import com.team.TeamUp.persistance.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TaskUtils {

    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private DTOsConverter dtOsConverter;

    public List<TaskDTO> getFilteredTasksByType(User user, String term, String type){
        List<Task> tasks = new ArrayList<>();
        if(type != null && type.toLowerCase().equals("assignedto")){
            tasks = taskRepository.findAllByAssigneesContaining(user);
        }else if(type != null && type.toLowerCase().equals("assignedby")){
            tasks = taskRepository.findAllByReporter(user);
        }

        if(term == null){
            return tasks.stream()
                    .map(task -> dtOsConverter.getDTOFromTask(task))
                    .collect(Collectors.toList());
        }

        return tasks.stream()
                .filter(task -> task.getSummary().toLowerCase().contains(term.toLowerCase()) ||
                        task.getDescription().toLowerCase().contains(term.toLowerCase()))
                .map(task -> dtOsConverter.getDTOFromTask(task))
                .collect(Collectors.toList());
    }

    public List<TaskDTO> getLastNTasks(long numberOfTasksToReturn){
        long totalNumberOfTasls = taskRepository.count();
        long numberOfWholePages = totalNumberOfTasls / 10;
        long tasksFromLastPage = totalNumberOfTasls % 10;

        if(numberOfTasksToReturn > totalNumberOfTasls){ //if there are more required than available, return all
            return getAllTasksConvertedToDTOs();
        }

        List<TaskDTO> tasks = new ArrayList<>();

        if(tasksFromLastPage != 0){ //if there are tasks that do not make a whole page
            tasks.addAll(taskRepository.findAll(PageRequest.of(Math.toIntExact(numberOfWholePages), 10))
                    .stream() //get all of them
                    .sorted((o1, o2) -> o2.getId() - o1.getId()) //sort descending
                    .limit(numberOfTasksToReturn % 10)//limit the number if request is lower
                    .map(dtOsConverter::getDTOFromTask)
                    .collect(Collectors.toList()));
            numberOfWholePages--;
            numberOfTasksToReturn -= numberOfTasksToReturn % 10;
        }
        while(numberOfTasksToReturn >= 10){ //while i can get whole pages
            tasks.addAll(taskRepository.findAll(PageRequest.of(Math.toIntExact(numberOfWholePages), 10))
                    .stream()
                    .sorted((o1, o2) -> o2.getId() - o1.getId()) //sort descending
                    .map(dtOsConverter::getDTOFromTask)
                    .collect(Collectors.toList()));
            numberOfWholePages--;
            numberOfTasksToReturn -= 10;
        }
        if(numberOfTasksToReturn != 0){ //if the number remaining does not represent a whole page
            List<TaskDTO> lastTasks = taskRepository.findAll(PageRequest.of(Math.toIntExact(numberOfWholePages), 10))
                    .stream()
                    .limit(numberOfTasksToReturn)
                    .map(dtOsConverter::getDTOFromTask)
                    .collect(Collectors.toList());
            tasks.addAll(lastTasks);
        }
        return tasks;
    }

    public List<TaskDTO> getAllTasksConvertedToDTOs(){
        return taskRepository.findAll().stream()
                .map(dtOsConverter::getDTOFromTask)
                .collect(Collectors.toList());
    }
}
