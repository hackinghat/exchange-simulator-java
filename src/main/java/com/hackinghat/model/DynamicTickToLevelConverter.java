package com.hackinghat.model;

import com.hackinghat.util.FloatUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


//auto valid_range = std::vector<std::tuple<level_t, level_t, level_t>>{
//        {0.0f, 0.9999f, 0.0001f},
//        {1.0f, 4.9995f, 0.0005f},
//        {5.0f, 9.999f, 0.001f},
//        {10.0f, 49.995f, 0.005f},
//        {50.0f, 99.99f, 0.01f},
//        };

public class DynamicTickToLevelConverter implements TickConverter {
    private final static Logger LOG = LogManager.getLogger(DynamicTickToLevelConverter.class);

    public static DynamicTickToLevelConverter DEFAULT_DYNAMIC_TICK_TO_LEVEL_CONVERTER = new DynamicTickToLevelConverter(new TickRange[]{
            new TickRange(0.f, 0.9999f, 0.0001f),
            new TickRange(1.f, 4.9995f, 0.0005f),
            new TickRange(5.f, 9.999f, 0.001f),
            new TickRange(10.f, 49.995f, 0.005f),
            new TickRange(50.f, 99.99f, 0.01f),
    });
    private final List<Level> ranges;

    public DynamicTickToLevelConverter(final TickRange[] _ranges) {
        Objects.requireNonNull(_ranges);
        if (_ranges.length == 0)
            throw new IllegalArgumentException("No ranges supplied");
        this.ranges = new ArrayList<>(_ranges.length);
        Level last = new Level(0.f, 0, _ranges[0].getTickSize());
        ranges.add(last);
        for (final TickRange tr : _ranges) {
            Level next = new Level(tr.getUpperBound(), last.getLevel() + Math.round((tr.getUpperBound() - tr.getLowerBound()) / tr.getTickSize()), tr.getTickSize());
            ranges.add(next);
            last = next;
            LOG.debug("Added level to tick converter: " + next);
        }
    }

    @Override
    public Level calculateLevel(final float px) {
        Level last = ranges.get(0);
        for (int i = 1; i < ranges.size(); ++i) {
            final Level l = ranges.get(i);
            if (FloatUtil.almost_lt_eps(px, l.getPrice(), l.getTickSize())) {
                return new Level(px, last.getLevel() + Math.round((px - last.getLevel()) / last.getTickSize()), last.getTickSize());
            }
        }
        return new Level(px, last.getLevel() + Math.round((last.getLevel() - px) / last.getTickSize()), last.getTickSize());
    }

    @Override
    public int calculateLevelIndex(final float px) {
        return calculateLevel(px).getLevel();
    }

    @Override
    public float calculatePrice(final Integer levelIndex) {
        return 0.f;
    }

    @Override
    public float roundToTick(float price) {
        return 0;
    }

    @Override
    public float getTickSize(float price) {
        return calculateLevel(price).getTickSize();
    }
}
