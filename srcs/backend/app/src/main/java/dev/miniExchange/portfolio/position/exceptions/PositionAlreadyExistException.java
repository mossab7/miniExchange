package dev.miniExchange.portfolio.position.exceptions;

import dev.miniExchange.common.exceptions.ResourceAlreadyExistException;

public class PositionAlreadyExistException extends ResourceAlreadyExistException
{
    public PositionAlreadyExistException()
    {
        super("position already exist");
    }
}
