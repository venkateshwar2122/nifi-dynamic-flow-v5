package com.example.nifi.flow.layout;

import com.example.nifi.flow.model.ProcessorPosition;
import org.springframework.stereotype.Service;

@Service
public class FlowLayoutService {

    private static final double START_X = 300;
    private static final double START_Y = 300;
    private static final double HORIZONTAL_GAP = 600;
    private static final double VERTICAL_GAP = 250;

    public ProcessorPosition horizontal(int index) {
        return new ProcessorPosition(
                START_X + (index * HORIZONTAL_GAP),
                START_Y
        );
    }

    public ProcessorPosition grid(int index) {
        int column = index % 4;
        int row = index / 4;

        return new ProcessorPosition(
                START_X + (column * HORIZONTAL_GAP),
                START_Y + (row * VERTICAL_GAP)
        );
    }
}