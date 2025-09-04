package nl.knaw.huc.di.elaborate.elabctl.archiver

import nl.knaw.huygens.tei.Comment
import nl.knaw.huygens.tei.CommentHandler
import nl.knaw.huygens.tei.Traversal
import nl.knaw.huygens.tei.XmlContext

class IgnoreCommentHandler : CommentHandler<XmlContext> {
    override fun visitComment(comment: Comment?, context: XmlContext?): Traversal = Traversal.NEXT
}

class KeepCommentHandler : CommentHandler<XmlContext> {
    override fun visitComment(comment: Comment?, context: XmlContext?): Traversal {
        context?.addLiteral(comment)
        return Traversal.NEXT
    }
}